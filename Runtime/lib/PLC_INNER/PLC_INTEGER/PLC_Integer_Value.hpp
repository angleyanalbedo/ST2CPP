#ifndef PLC_INTEGER_VALUE_HPP_
#define PLC_INTEGER_VALUE_HPP_

/***************************************************************
 * @file       PLC_Integer_Value.h
 * @brief      PLC_Integer_Value类是所有整型值类的基类
 * @author     chh
 * @version    0.2
 * @date       2022.4.25
 **************************************************************/

#include"../PLC_Inner_Value.h"

template<typename T, int TYPEID>
class PLC_Integer_Value : public PLC_Inner_Value
{
	public:
		/*值结构,si: short int*/
		T _data;

    public:
		/***************************************************************
		 *  @brief     	根据具体值/默认值构造
		 *  @param      data:值类的初始值，默认为0
		 *  @param      str_name:值类的名称，默认为"SINT"
		 * 	@modify     添加了str_name
		 *  @reason     值类缺少在构造时修改值类名称的方法
		 * 	@date       2022.04.17
		 **************************************************************/
    	PLC_Integer_Value(T data = 0, std::string str_name = "") :
			PLC_Inner_Value(TYPEID, str_name),
			_data(data)
		{}

		/***************************************************************
		 *  @brief     	拷贝构造
		 *  @param      resource:源值类的引用
		 **************************************************************/
    	PLC_Integer_Value(PLC_Integer_Value<T, TYPEID>* resource, std::string str_name = "") :
			PLC_Inner_Value(TYPEID, str_name),
			_data(resource->getValue())
		{}

	public:
		/***************************************************************
		 *  @brief     	直接设置data对象内的值 int内的值
		 *  @param      data:新的值
		 **************************************************************/
		void setValue(T data)
		{
			this->_data = (T)data;
		}


        void setValue(PLC_Integer_Value<T, TYPEID> value = PLC_Integer_Value<T, TYPEID>(0)){
            this->setValue(value.getValue());
        }

        /***************************************************************
		 *  @brief     	返回整形值类所包含的值
         *  @return     整型值类包含的值
		 **************************************************************/
		T getValue()
		{
			return this->_data;
		}

	private:

		/***************************************************************
		 *  @brief     	从传入参数（PLC类型或者基本类型）中找到可计算值并返回
         *  @return     类包含的值
		 **************************************************************/
		template<typename T1>
		T _getCalculableValue(T1& resource){
			using namespace std;
			try{
				if(is_base_of<PLC_Inner_Value, T1>::value)
				{
					return resource.getValue();
				}
				else
				{
					throw PLC_Exception("calc with a not numerical value");
				}
			}
			catch(PLC_Exception& exception)
			{
				exception.show();
			}
			
		}


	public:

		/***************************************************************
		 *  @brief     	操作符重载
		 **************************************************************/
		template<typename T1>
		PLC_Integer_Value<T, TYPEID> operator+(T1& another)
		{
			T si_result = this->getValue() + this->_getCalculableValue(another);
			auto* _temp = new PLC_Integer_Value<T, TYPEID>(si_result);
			return *_temp;
		}

        PLC_Integer_Value<T, TYPEID> operator+(T another)
        {
            T si_result = this->getValue() + another;
            auto* _temp = new PLC_Integer_Value<T, TYPEID>(si_result);
            return *_temp;
        }


		template<typename T1>
		PLC_Integer_Value<T, TYPEID> operator-(T1& another)
		{
			T si_result = this->getValue() - this->_getCalculableValue(another);
            auto* _temp = new PLC_Integer_Value<T, TYPEID>(si_result);
            return *_temp;
		}

        PLC_Integer_Value<T, TYPEID> operator-(T another)
        {
            T si_result = this->getValue() - another;
            auto* _temp = new PLC_Integer_Value<T, TYPEID>(si_result);
            return *_temp;
        }

		template<typename T1>
		PLC_Integer_Value<T, TYPEID> operator*(T1& another)
		{
			T si_result = this->getValue() * this->_getCalculableValue(another);
            auto* _temp = new PLC_Integer_Value<T, TYPEID>(si_result);
            return *_temp;
		}

        PLC_Integer_Value<T, TYPEID> operator*(T another)
        {
            T si_result = this->getValue() * another;
            auto* _temp = new PLC_Integer_Value<T, TYPEID>(si_result);
            return *_temp;
        }


		template<typename T1>
		PLC_Integer_Value<T, TYPEID> operator/(T1& another)
		{
			T si_result = this->getValue() / this->_getCalculableValue(another);
            auto* _temp = new PLC_Integer_Value<T, TYPEID>(si_result);
            return *_temp;
		}

        PLC_Integer_Value<T, TYPEID> operator/(T another)
        {
            T si_result = this->getValue() / another;
            auto* _temp = new PLC_Integer_Value<T, TYPEID>(si_result);
            return *_temp;
        }

		template<typename T1>
        PLC_Integer_Value<T, TYPEID>& operator=(T1& another)
		{
			this->setValue(this->_getCalculableValue(another));
            return *this;
		}

        PLC_Integer_Value<T, TYPEID>& operator=(T another)
        {
            this->setValue(another);
            return *this;
        }

    template<typename T1>
    int operator>(T1& another){
        return this->getValue() > this->_getCalculableValue(another);
    }

    template<typename T1>
    int operator>=(T1& another){
        return this->getValue() >= this->_getCalculableValue(another);
    }

    template<typename T1>
    int operator<(T1& another){
        return this->getValue() < this->_getCalculableValue(another);
    }

    template<typename T1>
    int operator<=(T1& another){
        return this->getValue() <= this->_getCalculableValue(another);
    }

    template<typename T1>
    int operator==(T1& another){
        return this->getValue() == this->_getCalculableValue(another);
    }

    template<typename T1>
    int operator!=(T1& another){
        return this->getValue() != this->_getCalculableValue(another);
    }

		/***************************************************************
		 *  @brief     	打印信息
		 **************************************************************/
		std::string toString()
		{
			return std::to_string(this->getValue());
		}
};


#endif